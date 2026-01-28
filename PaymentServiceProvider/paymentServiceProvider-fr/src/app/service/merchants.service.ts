import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Merchant, CreateMerchantDTO } from '../models/merchant.model';
import { PaymentMethod, Subscription } from '../models/payment_method.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MerchantService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  // Merchant Methods
  getAll(): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(`${this.apiUrl}/merchants`);
  }

  create(dto: CreateMerchantDTO): Observable<Merchant> {
    return this.http.post<Merchant>(`${this.apiUrl}/merchants`, dto);
  }

  // Subscription Methods
  getSubscriptions(merchantId: number): Observable<Subscription[]> {
    return this.http.get<Subscription[]>(`${this.apiUrl}/subscriptions/merchant/${merchantId}`);
  }

  getAvailableSubscriptions(merchantId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/subscriptions/available/${merchantId}`);
  }

  updateSubscription(
    subId: number,
    payload: { enabled?: boolean; configJson?: string },
  ): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/subscriptions/${subId}`, payload);
  }

  createSubscription(merchantId: number, paymentMethod: any): Observable<any> {
    const dto = {
      merchantAccountNumber: paymentMethod.merchantAccountNumber,
      configJson: paymentMethod.configJson,
      paymentMethodId: paymentMethod.id,
    };

    return this.http.post<any>(`${this.apiUrl}/subscriptions/merchant/${merchantId}`, dto);
  }

  // Payment methods
  getPaymentMethods(): Observable<PaymentMethod[]> {
    return this.http.get<PaymentMethod[]>(`${this.apiUrl}/payment-methods`);
  }
}
