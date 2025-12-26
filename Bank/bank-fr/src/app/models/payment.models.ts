export interface PaymentDetailsResponse {
  amount: number;
  currency: string;
  merchantName: string;
  expiresAt: string; 
  acceptedCardTypes: string[];
  expired: boolean;
  used: boolean;
}

export interface PaymentProcessRequest {
  pan: string;
  securityCode: string;
  cardHolderName: string;
  expirationDate: string; 
}

export interface PaymentProcessResponse {
  success: boolean;
  message: string;
  globalTransactionId: string | null;
}

export type CardType = 'VISA' | 'MASTERCARD' | null;


