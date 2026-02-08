import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

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
  private apiUrl = environment.apiBaseUrl + '/insurances';

  constructor(private http: HttpClient) {}

  getAllInsurances(): Observable<Insurance[]> {
    return this.http.get<Insurance[]>(this.apiUrl);
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

