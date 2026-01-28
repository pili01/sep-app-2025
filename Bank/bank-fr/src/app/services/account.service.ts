import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly apiUrl: string = environment.apiBaseUrl + '/bank/accounts';

  constructor(private http: HttpClient) {}

  loadAccountData(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/my`);
  }
}
