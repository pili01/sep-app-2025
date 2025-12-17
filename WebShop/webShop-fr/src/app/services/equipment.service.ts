import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';

export interface Equipment {
  id?: number;
  name: string;
  description?: string;
  pricePerDay: number;
}

@Injectable({
  providedIn: 'root'
})
export class EquipmentService {
  private apiUrl = 'https://localhost:8441/api/equipment';

  constructor(private http: HttpClient) {}

  getAllEquipment(): Promise<Equipment[]> {
    return firstValueFrom(this.http.get<Equipment[]>(this.apiUrl));
  }

  getEquipmentById(id: number): Observable<Equipment> {
    return this.http.get<Equipment>(`${this.apiUrl}/${id}`);
  }

  createEquipment(equipment: Omit<Equipment, 'id'>): Observable<Equipment> {
    return this.http.post<Equipment>(this.apiUrl, equipment);
  }

  updateEquipment(id: number, equipment: Omit<Equipment, 'id'>): Observable<Equipment> {
    return this.http.put<Equipment>(`${this.apiUrl}/${id}`, equipment);
  }

  deleteEquipment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

