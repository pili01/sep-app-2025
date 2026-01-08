import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PaymentStatusResponse {
  transactionId: string;
  status: string;
  rentalId: number;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = environment.apiBaseUrl + '/payment';

  constructor(private http: HttpClient) { }

  getPaymentStatus(transactionId: string): Observable<PaymentStatusResponse> {
    return this.http.get<PaymentStatusResponse>(`${this.apiUrl}/status?transactionId=${transactionId}`);
  }
}

