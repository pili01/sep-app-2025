import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaymentDetailsResponse, PaymentProcessRequest, PaymentProcessResponse } from '../models/payment.models';
import { QrRawData } from '../components/pay-with-qr/pay-with-qr';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = 'https://localhost:8443/api/bank/payment';

  constructor(private http: HttpClient) {}

  getPaymentDetails(paymentId: string): Observable<PaymentDetailsResponse> {
    return this.http.get<PaymentDetailsResponse>(`${this.apiUrl}/${paymentId}`);
  }

  
  processPayment(paymentId: string, request: PaymentProcessRequest): Observable<PaymentProcessResponse> {
    return this.http.post<PaymentProcessResponse>(`${this.apiUrl}/${paymentId}/process`, request);
  }


 processPaymentQR(request:QrRawData): Observable<PaymentProcessResponse> {
    return this.http.post<PaymentProcessResponse>(`${this.apiUrl}/processQR`, request);
  }

}


