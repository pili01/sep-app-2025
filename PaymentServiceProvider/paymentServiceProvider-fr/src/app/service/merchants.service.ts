import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Merchant, CreateMerchantDTO } from '../models/merchant.model';

@Injectable({ providedIn: 'root' })
export class MerchantService {
  private apiUrl = 'https://localhost:8442/api/merchants';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Merchant[]> {
    return this.http.get<Merchant[]>(this.apiUrl);
  }

  create(dto: CreateMerchantDTO): Observable<Merchant> {
    return this.http.post<Merchant>(this.apiUrl, dto);
  }
}