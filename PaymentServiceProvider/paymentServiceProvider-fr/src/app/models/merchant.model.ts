export enum MerchantStatus {
    DRAFT = 'DRAFT',
    ACTIVE = 'ACTIVE',
    INACTIVE = 'INACTIVE'
}

export interface Merchant {
    id?: number;            
    merchantId: string;      
    merchantPassword: string; 
    name: string;
    status: MerchantStatus;
    successUrl: string;
    failedUrl: string;
    errorUrl: string;
}

export interface CreateMerchantDTO {
    name: string;
    successUrl: string;
    failedUrl: string;
    errorUrl: string;
}