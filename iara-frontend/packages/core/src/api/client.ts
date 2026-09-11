// Cliente API compartilhado - Mobile + Web
import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { EncryptionService } from '@iara/utils/encryption';

const API_BASE_URL = __DEV__ 
  ? 'http://10.0.2.2:8080/api/v1'  // Android emulator
  : 'https://api.iara.app/api/v1';

class ApiClient {
  private client: AxiosInstance;
  private encryption: EncryptionService;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: { 'Content-Type': 'application/json' },
    });

    this.encryption = new EncryptionService();
    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request: Injeta Access Token + Device Fingerprint
    this.client.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
      const accessToken = await this.getAccessToken();
      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
      }
      config.headers['X-Device-Fingerprint'] = await this.getDeviceFingerprint();
      return config;
    });

    // Response: Rotaciona Refresh Token automaticamente
    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;
          const newToken = await this.refreshAccessToken();
          if (newToken) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return this.client(originalRequest);
          }
        }
        return Promise.reject(error);
      }
    );
  }

  // Criptografia client-side para campos sensíveis ANTES de enviar
  async encryptSensitiveFields<T extends Record<string, any>>(data: T, sensitiveFields: string[]): Promise<T> {
    const encrypted = { ...data };
    for (const field of sensitiveFields) {
      if (encrypted[field] && typeof encrypted[field] === 'string') {
        encrypted[field] = await this.encryption.encrypt(encrypted[field]);
      }
    }
    return encrypted;
  }

  // Descriptografa campos sensíveis na resposta
  decryptResponse<T>(response: T, sensitiveFields: string[]): T {
    const decrypted = { ...response } as any;
    for (const field of sensitiveFields) {
      if (decrypted[field] && typeof decrypted[field] === 'string') {
        try {
          decrypted[field] = this.encryption.decrypt(decrypted[field]);
        } catch {
          // Se falhar descriptografar, mantém como está (pode já estar em claro)
        }
      }
    }
    return decrypted;
  }

  // Endpoints tipados
  async getCurrentCycle(userId?: string) {
    const url = userId ? `/cycle/current/${userId}` : '/cycle/current';
    const response = await this.client.get<CyclePredictionResponse>(url);
    return response.data;
  }

  async logSymptom(data: LogSymptomRequest) {
    // Criptografa observações ANTES de enviar
    const encrypted = await this.encryptSensitiveFields(data, ['observacoes']);
    return this.client.post('/symptoms', encrypted);
  }

  // ... outros endpoints
}

export const apiClient = new ApiClient();