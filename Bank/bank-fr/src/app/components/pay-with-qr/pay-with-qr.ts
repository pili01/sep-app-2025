import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import jsQR from 'jsqr';
import { AccountService } from '../../services/account.service';
import { PaymentService } from '../../services/payment.service';

// RAW QR format (ono što dolazi iz QR-a)
export interface QrRawData {
  K?: string;   // paymentCode
  V?: string;   // version
  C?: string;   // characterSet
  R?: string;   // accountNumber
  N?: string;   // recipientName
  I?: string;   // amount
  SF?: string;  // paymentPurposeCode
  S?: string;   // paymentPurpose
  RO?: string;  // referenceNumber
}

// Tvoj postojeći “lep” format
export interface QrData {
  paymentCode?: string;
  version?: string;
  characterSet?: string;
  accountNumber?: string;
  recipientName?: string;
  amount?: string;
  paymentPurposeCode?: string;
  paymentPurpose?: string;
  referenceNumber?: string;
}

// Konverter RAW → QrData
export function mapQrRawToQrData(raw: QrRawData): QrData {
  return {
    paymentCode: raw.K,
    version: raw.V,
    characterSet: raw.C,
    accountNumber: raw.R,
    recipientName: raw.N,
    amount: raw.I,
    paymentPurposeCode: raw.SF,
    paymentPurpose: raw.S,
    referenceNumber: raw.RO
  };
}


@Component({
  selector: 'app-pay-with-qr',
  imports: [CommonModule, FormsModule],
  templateUrl: './pay-with-qr.html',
  styleUrl: './pay-with-qr.scss',
  standalone: true
})
export class PayWithQr implements OnInit {
  qrData = signal<QrData>({});
  previewUrl = signal<string | null>(null);
  error = signal<string | null>(null);

  // Podaci o računu korisnika
  userAccountNumber = signal<string>('');
  userName = signal<string>('');

  // Form fields
  accountNumber = signal<string>('');
  recipientName = signal<string>('');
  amount = signal<string>('');
  currency = signal<string>('RSD');
  paymentPurpose = signal<string>('');
  referenceNumber = signal<string>('');

  constructor(
    private http: HttpClient,
    private activatedRoute: ActivatedRoute,
    private authService: AuthService,
    private accountService: AccountService,
    private paymentService:PaymentService
  ) { }

  ngOnInit(): void {
    this.loadUserData();
    this.loadUrlParams();
  }

  loadUserData(): void {
    // Učitaj podatke o računu korisnika iz servera
    const currentUser = this.authService.currentUser();
    if (currentUser?.userId) {
      // Učitaj podatke o korisnikovom računu
      this.accountService.loadAccountData()
        .subscribe({
          next: (data: any) => {
            if (data) {
              this.userAccountNumber.set(data.accountNumber || '');
              this.userName.set(data.accountHolderName || `${data.firstName || ''} ${data.lastName || ''}`.trim());

              // Ako nema podataka iz QR koda, koristi podatke korisnika
              if (!this.accountNumber()) {
                //alert('Greška pri učitavanju podataka o računu korisnika.');
              }
            }
          },
          error: (err) => {
            console.error('Greška pri učitavanju računa:', err);
          }
        });
    }
  }

  loadUrlParams(): void {
    // Učitaj parametre iz URL-a (mBank aplikacija može prosllediti podatke)
    this.activatedRoute.queryParams.subscribe(params => {
      if (params['accountNumber']) {
        this.recipientName.set(params['recipientName'] || this.recipientName());
        this.amount.set(params['amount'] || this.amount());
        this.currency.set(params['currency'] || 'RSD');
        this.paymentPurpose.set(params['paymentPurpose'] || this.paymentPurpose());
        this.referenceNumber.set(params['referenceNumber'] || this.referenceNumber());
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];

      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.error.set('Molimo izaberite sliku');
        return;
      }

      this.error.set(null);
      this.processQrImage(file);
    }
  }

  processQrImage(file: File): void {
    const reader = new FileReader();

    reader.onload = (e: ProgressEvent<FileReader>) => {
      const imageUrl = e.target?.result as string;
      this.previewUrl.set(imageUrl);

      // Create image element to read pixels
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');

        if (!ctx) {
          this.error.set('Greška pri obradi slike');
          return;
        }

        canvas.width = img.width;
        canvas.height = img.height;
        ctx.drawImage(img, 0, 0);

        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);

        if (code) {
          this.parseQrData(code.data);
        } else {
          this.error.set('Nije pronađen QR kod na slici');
        }
      };

      img.onerror = () => {
        this.error.set('Greška pri učitavanju slike');
      };

      img.src = imageUrl;
    };

    reader.readAsDataURL(file);
  }

  parseQrData(qrText: string): void {
    console.log('QR Data:', qrText);

    const data: QrData = {};
    const parts = qrText.split('|');

    parts.forEach(part => {
      const [key, value] = part.split(':');
      switch (key) {
        case 'K':
          data.paymentCode = value;
          break;
        case 'V':
          data.version = value;
          break;
        case 'C':
          data.characterSet = value;
          break;
        case 'R':
          data.accountNumber = value;
          this.accountNumber.set(value);
          break;
        case 'N':
          data.recipientName = value;
          this.recipientName.set(value);
          break;
        case 'I':
          data.amount = value;
          // Parse amount (format: RSD2,95)
          const amountMatch = value.match(/([A-Z]{3})([0-9,\.]+)/);
          if (amountMatch) {
            this.currency.set(amountMatch[1]);
            this.amount.set(amountMatch[2].replace(',', '.'));
          }
          break;
        case 'SF':
          data.paymentPurposeCode = value;
          break;
        case 'S':
          data.paymentPurpose = value;
          this.paymentPurpose.set(value);
          break;
        case 'RO':
          data.referenceNumber = value;
          this.referenceNumber.set(value);
          break;
      }
    });

    this.qrData.set(data);
  }

  onSubmit(): void {
    // Validacija
    if (!this.accountNumber() || !this.recipientName() || !this.amount()) {
      this.error.set('Molimo popunite sva obavezna polja');
      return;
    }

    const request: QrRawData = {
    R: this.accountNumber(),
    N: this.recipientName(),
    I: this.amount(),
    S: this.paymentPurpose(),
    RO: this.referenceNumber()
  };
  console.log('QR request:', request);

    this.paymentService.processPaymentQR(request).subscribe({
    next: (response) => {
      console.log('Payment success:', response);
      if(response.redirectUrl){
        window.location.href=response.redirectUrl
      }
      this.resetForm();
    },
    error: (err) => {
      console.error('Payment failed:', err);
      this.error.set('Greška prilikom obrade plaćanja.');
    }
  });
  }

  resetForm(): void {
    this.qrData.set({});
    this.previewUrl.set(null);
    this.error.set(null);
    this.accountNumber.set('');
    this.recipientName.set('');
    this.amount.set('');
    this.currency.set('RSD');
    this.paymentPurpose.set('');
    this.referenceNumber.set('');
  }
}
