// Criptografia client-side para dados sensíveis ANTES de sair do dispositivo
// Usa Web Crypto API (Web) ou react-native-quick-crypto (Mobile)

export class EncryptionService {
  private static readonly ALGORITHM = 'AES-GCM';
  private static readonly KEY_LENGTH = 256;
  private static readonly IV_LENGTH = 12;
  private static readonly SALT_LENGTH = 16;

  private masterKey: CryptoKey | null = null;

  async initialize(userPassword: string, salt?: Uint8Array): Promise<void> {
    const encoder = new TextEncoder();
    const passwordKey = await crypto.subtle.importKey(
      'raw',
      encoder.encode(userPassword),
      'PBKDF2',
      false,
      ['deriveKey']
    );

    const derivedKey = await crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt: salt || crypto.getRandomValues(new Uint8Array(this.SALT_LENGTH)),
        iterations: 100000,
        hash: 'SHA-256',
      },
      passwordKey,
      { name: this.ALGORITHM, length: this.KEY_LENGTH },
      false,
      ['encrypt', 'decrypt']
    );

    this.masterKey = derivedKey;
  }

  async encrypt(plaintext: string): Promise<string> {
    if (!this.masterKey) throw new Error('Encryption not initialized');

    const iv = crypto.getRandomValues(new Uint8Array(this.IV_LENGTH));
    const encoded = new TextEncoder().encode(plaintext);

    const ciphertext = await crypto.subtle.encrypt(
      { name: this.ALGORITHM, iv },
      this.masterKey,
      encoded
    );

    // Combina: salt + iv + ciphertext (Base64)
    const combined = new Uint8Array(this.SALT_LENGTH + this.IV_LENGTH + ciphertext.byteLength);
    // ... implementação completa
    return btoa(String.fromCharCode(...combined));
  }

  async decrypt(encrypted: string): Promise<string> {
    if (!this.masterKey) throw new Error('Encryption not initialized');
    // ... implementação inversa
    return plaintext;
  }
}

// Hook para usar no React
export function useEncryption() {
  const [service] = useState(() => new EncryptionService());
  return service;
}