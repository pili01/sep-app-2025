export interface PaymentMethod {
    id?: number;            
    name: String;
    paymentMethodCode: String
    active: Boolean
    lastHeartbeat: Date;
}

export interface Subscription {
  id: number;
  merchantId: number;
  paymentMethodId: number;
  paymentMethodName: string;
  paymentMethodCode: string;   
  enabled: boolean;
  active: boolean;
  configJson: string;
  createdAt: string;       
}