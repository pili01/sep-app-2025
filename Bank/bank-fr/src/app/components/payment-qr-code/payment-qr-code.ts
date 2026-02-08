import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-payment-qr-code',
  imports: [CommonModule],
  templateUrl: './payment-qr-code.html',
  styleUrl: './payment-qr-code.scss',
  standalone: true,
})
export class PaymentQrCode implements OnInit {
  paymentId: string | null = null;
  qrCodeUrl = signal<SafeUrl | null>(null);
  qrCodeBlob: Blob | null = null;
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  private apiUrl = environment.apiBaseUrl + '/bank/qr';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.paymentId = this.route.snapshot.paramMap.get('paymentId');
    if (this.paymentId) {
      this.loadQrCode();
    } else {
      this.error.set('Payment ID is missing');
      this.loading.set(false);
    }
  }

  loadQrCode(): void {
    if (!this.paymentId) return;

    this.http
      .post(
        `${this.apiUrl}/generate/${this.paymentId}`,
        {},
        {
          responseType: 'blob',
          observe: 'response',
        },
      )
      .subscribe({
        next: (response) => {
          if (response.body) {
            this.qrCodeBlob = response.body;
            const objectUrl = URL.createObjectURL(response.body);
            this.qrCodeUrl.set(this.sanitizer.bypassSecurityTrustUrl(objectUrl));
            this.loading.set(false);
          }
        },
        error: (err) => {
          console.error('Error loading QR code:', err);
          this.error.set('Greška pri učitavanju QR koda');
          this.loading.set(false);
        },
      });
  }

  downloadQrCode(): void {
    if (!this.qrCodeBlob) return;

    const url = URL.createObjectURL(this.qrCodeBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `qr-code-${this.paymentId}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}
