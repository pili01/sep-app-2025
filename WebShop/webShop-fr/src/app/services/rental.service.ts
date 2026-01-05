import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Equipment } from './equipment.service';
import { environment } from '../../environments/environment';

export interface RentalRequest {
  vehicleId: number;
  startDate: string; // ISO format: "2025-12-20T10:00:00"
  endDate: string;
  insuranceId: number;
  equipment: Equipment[];
}

export interface Rental {
  id?: number;
  vehicleId: number;
  vehicle?: {
    id?: number;
    type: string;
    registration: string;
    chassisNumber: string;
    pricePerDay: number;
    pictureUrl?: string;
  };
  user?: {
    id?: number;
    email: string;
    firstName?: string;
    lastName?: string;
  };
  startDate: string;
  endDate: string;
  totalPrice: number;
  insuranceId: number;
  insurance?: {
    id?: number;
    company: string;
    name: string;
    pricePerDay: number;
  };
  status: 'DRAFT' | 'PURCHASED';
  equipment?: Array<{
    id?: number;
    name: string;
    description?: string;
    pricePerDay: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class RentalService {
  private apiUrl = environment.apiBaseUrl + '/rental';

  constructor(private http: HttpClient) { }

  createRental(rental: RentalRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/`, rental, {
      responseType: 'text'
    }) as Observable<string>;
  }

  getMyRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(`${this.apiUrl}/my`) as Observable<Rental[]>;
  }

  getAllRentals(): Observable<Rental[]> {
    return this.http.get<Rental[]>(`${this.apiUrl}/all`) as Observable<Rental[]>;
  }

  cancelRental(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      responseType: 'text'
    }) as Observable<string>;
  }

  payRental(id: number): Observable<string> {
    return this.http.patch(`${this.apiUrl}/${id}/pay`, {}, { responseType: 'text' }) as Observable<string>;
  }
}


