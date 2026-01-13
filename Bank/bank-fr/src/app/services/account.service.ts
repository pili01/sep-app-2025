import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = 'https://localhost:8443/api/bank/accounts';

  constructor(private http: HttpClient) {}

  loadAccountData(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/my`);
  }
}


