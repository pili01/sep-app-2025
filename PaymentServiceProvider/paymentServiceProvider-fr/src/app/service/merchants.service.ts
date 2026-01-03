import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Merchant, CreateMerchantDTO } from '../models/merchant.model';

@Injectable({ providedIn: 'root' })
export class MerchantService {
  private apiUrl = 'https://localhost:8442/api'; // Base API

  constructor(private http: HttpClient) {}

  // Merchant Methods
  getAll(): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(`${this.apiUrl}/merchants`);
  }

  create(dto: CreateMerchantDTO): Observable<Merchant> {
    return this.http.post<Merchant>(`${this.apiUrl}/merchants`, dto);
  }

  // Subscription Methods
  getSubscriptions(merchantId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/subscriptions/merchant/${merchantId}`);
  }

  updateSubscription(subId: number, payload: { enabled?: boolean; configJson?: string }): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/subscriptions/${subId}`, payload);
  }

  createSubscription(merchantId: number, dto: { paymentMethodCode: string, configJson: string }): Observable<any> {
  return this.http.post<any>(`${this.apiUrl}/subscriptions/merchant/${merchantId}`, dto);
  }
}