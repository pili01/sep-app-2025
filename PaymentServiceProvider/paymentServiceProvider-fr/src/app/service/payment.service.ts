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

  // deprecated 
  // getSubscribedPaymentMethods(merchantId: string): Observable<any> {
  //   return this.http.get<any>(`${this.apiUrl}/subscriptions/${merchantId}`);
  // }

  initiatePayment(transactionId: string, subscriptionId: number): Observable<any> {
    const payload = { transactionId, subscriptionId };
    return this.http.post<any>(`${this.apiUrl}/initiate`, payload);
  }
}
