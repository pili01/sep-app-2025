import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly apiUrl = environment.apiBaseUrl + '/payment';
  private readonly paymentMethodUrl = environment.apiBaseUrl + '/payment-methods';

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

  // Payment Method Management
  getAllPaymentMethods(): Observable<any[]> {
    return this.http.get<any[]>(`${this.paymentMethodUrl}`);
  }

  addPaymentMethod(request: any): Observable<any> {
    return this.http.post<any>(`${this.paymentMethodUrl}`, request);
  }

  updatePaymentMethod(id: number, request: any): Observable<any> {
    return this.http.put<any>(`${this.paymentMethodUrl}/${id}`, request);
  }

  deletePaymentMethod(id: number): Observable<any> {
    return this.http.delete<any>(`${this.paymentMethodUrl}/${id}`);
  }

  getPaymentMethodById(id: number): Observable<any> {
    return this.http.get<any>(`${this.paymentMethodUrl}/${id}`);
  }

  // Icon upload
  uploadPaymentMethodIcon(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.paymentMethodUrl}/upload-icon`, formData);
  }

  // Toggle active status
  togglePaymentMethodActive(id: number): Observable<any> {
    return this.http.patch<any>(`${this.paymentMethodUrl}/${id}/toggle`, {});
  }
}
