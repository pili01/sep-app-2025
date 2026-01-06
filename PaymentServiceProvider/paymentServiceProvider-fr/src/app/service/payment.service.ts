import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = 'https://localhost:8442/api/payment';

  constructor(private http: HttpClient) {}

  initializePayment(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/init`, request);
  }

  getAvailablePaymentMethods(merchantId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/subscriptions/${merchantId}`);
  }

  initiatePayment(transactionId: string, paymentMethodCode: string): Observable<any> {
    const payload = { transactionId, paymentMethodCode };
    return this.http.post<any>(`${this.apiUrl}/initiate`, payload);
  }
}
