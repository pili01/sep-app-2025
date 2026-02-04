import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

interface CryptoPaymentData {
  transactionId: string;
  status: string;
  amount: number;
  currency: string;
  bitcoinAmount: number;
  bitcoinAddress: string;
  confirmations: number;
  createdAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

@Component({
  selector: 'app-crypto-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './crypto-payment.component.html',
  styleUrl: './crypto-payment.component.scss',
})
export class CryptoPaymentComponent implements OnInit, OnDestroy {
  transactionId: string | null = null;
  isLoading = signal(true);
  error = signal<string | null>(null);
  paymentData = signal<CryptoPaymentData | null>(null);
  private pollingSubscription?: Subscription;

  private readonly cryptoApiUrl = 'https://localhost:8445/api/payment';
  private readonly POLLING_INTERVAL = 30000; // 30 sekundi (smanjeno zbog rate limit-a)

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.transactionId = this.route.snapshot.paramMap.get('transactionId');
    
    if (!this.transactionId) {
      this.error.set('Transaction ID not found');
      this.isLoading.set(false);
      return;
    }

    // Učitaj podatke odmah
    this.loadPaymentData();

    // Pokreni automatsko osvežavanje statusa svakih 10 sekundi
    this.startPolling();
  }

  ngOnDestroy(): void {
    // Zaustavi polling kada se komponenta uništi
    this.stopPolling();
  }

  loadPaymentData(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.http.get<CryptoPaymentData>(`${this.cryptoApiUrl}/status/${this.transactionId}`).subscribe({
      next: (data) => {
        this.paymentData.set(data);
        this.isLoading.set(false);

        // Ako je payment COMPLETED, preusmeri korisnika na success URL
        if (data.status === 'COMPLETED' && data.completedAt) {
          this.handlePaymentCompleted();
        }
      },
      error: (err) => {
        console.error('Error loading payment data:', err);
        this.error.set('Failed to load payment information');
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Pokreće automatsko osvežavanje statusa svakih 30 sekundi
   * Zaustavlja se kada je payment COMPLETED ili FAILED
   * Smanjeno sa 10 sekundi zbog rate limit-a
   */
  private startPolling(): void {
    this.pollingSubscription = interval(this.POLLING_INTERVAL)
      .pipe(
        switchMap(() => 
          this.http.get<CryptoPaymentData>(`${this.cryptoApiUrl}/status/${this.transactionId}`)
        )
      )
      .subscribe({
        next: (data) => {
          const currentData = this.paymentData();
          
          // Ažuriraj podatke samo ako se status promenio
          if (!currentData || currentData.status !== data.status || 
              currentData.confirmations !== data.confirmations) {
            this.paymentData.set(data);
            
            // Ako je payment COMPLETED, preusmeri korisnika
            if (data.status === 'COMPLETED' && data.completedAt) {
              this.handlePaymentCompleted();
            }
          }
        },
        error: (err) => {
          console.error('Error polling payment status:', err);
          // Ne zaustavljaj polling zbog greške - možda je privremena
        }
      });
  }

  /**
   * Zaustavlja automatsko osvežavanje statusa
   */
  private stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = undefined;
    }
  }

  /**
   * Rukuje završetkom paymenta - preusmerava korisnika na success URL
   */
  private handlePaymentCompleted(): void {
    this.stopPolling(); // Zaustavi polling
    
    const paymentData = this.paymentData();
    if (paymentData) {
      // Uzmi success URL iz payment podataka (može biti u response-u ili iz PSP-a)
      // Za sada, preusmeri na WebShop success stranicu
      // TODO: Dobiti success URL iz PSP-a ili iz payment podataka
      
      // Možeš dodati delay pre preusmeravanja da korisnik vidi "Payment Completed" poruku
      setTimeout(() => {
        // Preusmeri na success stranicu
        // window.location.href = paymentData.successUrl; // Ako imaš success URL
        // Ili jednostavno prikaži poruku
        alert('Plaćanje je uspešno završeno!');
      }, 2000);
    }
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      alert('Copied to clipboard!');
    }).catch(err => {
      console.error('Failed to copy:', err);
    });
  }

  /**
   * Ručno osvežavanje statusa (kada korisnik klikne "Refresh Status" dugme)
   */
  refreshStatus(): void {
    this.loadPaymentData();
  }
}
