import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';

export interface Insurance {
  id?: number;
  company: string;
  name?: string;
  pricePerDay: number;
}

@Injectable({
  providedIn: 'root'
})
export class InsuranceService {
  private apiUrl = 'https://localhost:8441/api/insurances';

  constructor(private http: HttpClient) {}

  getAllInsurances(): Promise<Insurance[]> {
    return firstValueFrom(this.http.get<Insurance[]>(this.apiUrl));
  }

  getInsuranceById(id: number): Observable<Insurance> {
    return this.http.get<Insurance>(`${this.apiUrl}/${id}`);
  }

  createInsurance(insurance: Omit<Insurance, 'id'>): Observable<Insurance> {
    return this.http.post<Insurance>(this.apiUrl, insurance);
  }

  updateInsurance(id: number, insurance: Omit<Insurance, 'id'>): Observable<Insurance> {
    return this.http.put<Insurance>(`${this.apiUrl}/${id}`, insurance);
  }

  deleteInsurance(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

