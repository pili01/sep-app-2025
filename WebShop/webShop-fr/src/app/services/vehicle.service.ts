import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Vehicle {
  id?: number;
  pricePerDay: number;
  registration: string;
  chassisNumber: string;
  type: string;
  pictureUrl?: string;
}

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private apiUrl = 'https://localhost:8441/api/vehicles';

  constructor(private http: HttpClient) {}

  getAllVehicles(): Promise<Vehicle[]> {
    return this.http.get<Vehicle[]>(this.apiUrl).toPromise() as Promise<Vehicle[]>;
  }

  getVehicleById(id: number): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${this.apiUrl}/${id}`);
  }

  createVehicle(vehicle: Omit<Vehicle, 'id'>): Observable<Vehicle> {
    return this.http.post<Vehicle>(this.apiUrl, vehicle);
  }

  updateVehicle(id: number, vehicle: Omit<Vehicle, 'id'>): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.apiUrl}/${id}`, vehicle);
  }

  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

