import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  surname: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  email: string;
  userRole: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'https://localhost:8441';
  private tokenKey = 'auth_token';
  private userKey = 'user_data';

  isAuthenticated = signal<boolean>(false);
  currentUser = signal<LoginResponse | null>(null);

  constructor(private http: HttpClient) {
    this.loadAuthData();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials)
      .pipe(
        tap(response => {
          console.log(response);
          this.setAuthData(response);
        })
      );
  }

  register(userData: RegisterRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/auth/register`, userData, {
      responseType: 'text'
    }) as Observable<string>;
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthor(): boolean {
    const user = this.currentUser();
    return user?.userRole === 'AUTHOR';
  }

  isCustomer(): boolean {
    const user = this.currentUser();
    return user?.userRole === 'CUSTOMER';
  }

  private setAuthData(response: LoginResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.userKey, JSON.stringify(response));
    this.isAuthenticated.set(true);
    this.currentUser.set(response);
  }

  private loadAuthData(): void {
    const token = localStorage.getItem(this.tokenKey);
    const userData = localStorage.getItem(this.userKey);
    
    if (token && userData) {
      try {
        const user = JSON.parse(userData);
        this.isAuthenticated.set(true);
        this.currentUser.set(user);
      } catch (e) {
        this.logout();
      }
    }
  }
}

