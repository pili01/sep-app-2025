import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { CardValidationService } from '../../services/card-validation.service';
import { PaymentDetailsResponse, PaymentProcessRequest, CardType } from '../../models/payment.models';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.scss']
})
export class PaymentFormComponent implements OnInit, OnDestroy {
  paymentForm: FormGroup;
  paymentDetails: PaymentDetailsResponse | null = null;
  paymentId: string = '';
  cardType: CardType = null;
  timeRemaining: number = 0; // sekunde
  private timerSubscription?: Subscription;
  isProcessing = false;
  isLoading = true; // Loading state
  errorMessage: string | null = null; // Error message
  paymentResult: { success: boolean; message: string } | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private cardValidationService: CardValidationService,
    private cdr: ChangeDetectorRef
  ) {
    this.paymentForm = this.fb.group({
      pan: ['', [Validators.required, this.luhnValidator.bind(this)]],
      securityCode: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
      cardHolderName: ['', [Validators.required, Validators.minLength(2)]],
      expirationDate: ['', [Validators.required, this.expirationDateValidator.bind(this)]]
    });
  }

  ngOnInit(): void {
    this.paymentId = this.route.snapshot.paramMap.get('paymentId') || '';
    
    if (!this.paymentId) {
      this.router.navigate(['/']);
      return;
    }

    this.loadPaymentDetails();
    
    this.paymentForm.get('pan')?.valueChanges.subscribe(pan => {
      this.cardType = this.cardValidationService.detectCardType(pan);
      
      const formatted = this.cardValidationService.formatCardNumber(pan);
      if (formatted !== pan) {
        this.paymentForm.get('pan')?.setValue(formatted, { emitEvent: false });
      }
    });
    
    this.paymentForm.get('expirationDate')?.valueChanges.subscribe(value => {
      const digits = value.replace(/\D/g, '');
      if (digits.length <= 4) {
        let formatted = digits;
        if (digits.length >= 2) {
          formatted = digits.slice(0, 2) + '/' + digits.slice(2);
        }
        if (formatted !== value) {
          this.paymentForm.get('expirationDate')?.setValue(formatted, { emitEvent: false });
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.timerSubscription?.unsubscribe();
  }

  loadPaymentDetails(): void {
    this.isLoading = true;
    this.errorMessage = null;
    
    this.paymentService.getPaymentDetails(this.paymentId).subscribe({
      next: (details) => {
        this.isLoading = false;
        console.log('Payment details received:', details);
        console.log('Amount:', details.amount);
        console.log('Expired:', details.expired);
        console.log('Used:', details.used);
        this.paymentDetails = details;
        console.log('paymentDetails set to:', this.paymentDetails);
        
        if (details.expired || details.used) {
          console.log('Payment expired or used, disabling form');
          this.paymentForm.disable();
          if (details.expired) {
            this.paymentResult = { success: false, message: 'Payment session has expired' };
          } else if (details.used) {
            this.paymentResult = { success: false, message: 'Payment has already been processed' };
          }
          return;
        }
        
        console.log('Payment is valid, starting timer');
        this.startTimer(details.expiresAt);
        this.cdr.detectChanges();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error loading payment details:', err);
        console.error('Full error object:', JSON.stringify(err, null, 2));
        let errorMsg = 'Failed to load payment details';
        if (err.status === 0) {
          errorMsg = 'Cannot connect to backend. Make sure the backend is running on https://localhost:8443';
        } else if (err.status === 404) {
          errorMsg = `Payment transaction with ID "${this.paymentId}" not found. Please create a test transaction first.`;
        } else if (err.error?.message) {
          errorMsg = err.error.message;
        } else if (err.message) {
          errorMsg = err.message;
        }
        
        this.errorMessage = errorMsg;
        this.paymentResult = { success: false, message: errorMsg };
      }
    });
  }

  startTimer(expiresAt: string): void {
    const expiry = new Date(expiresAt);
    const updateTimer = () => {
      const now = new Date();
      const diff = Math.floor((expiry.getTime() - now.getTime()) / 1000);
      
      if (diff <= 0) {
        this.timeRemaining = 0;
        this.paymentForm.disable();
        this.paymentResult = { success: false, message: 'Payment session has expired' };
        this.timerSubscription?.unsubscribe();
        this.cdr.detectChanges();
      } else {
        this.timeRemaining = diff;
        this.cdr.detectChanges();
      }
    };
    
    updateTimer();
    this.timerSubscription = interval(1000).subscribe(() => updateTimer());
  }

  formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  luhnValidator(control: any) {
    if (!control.value) return null;
    const isValid = this.cardValidationService.validateLuhn(control.value.replace(/\s/g, ''));
    return isValid ? null : { invalidLuhn: true };
  }

  expirationDateValidator(control: any) {
    if (!control.value) return null;
    const isValid = this.cardValidationService.validateExpirationDate(control.value);
    return isValid ? null : { invalidExpirationDate: true };
  }

  onSubmit(): void {
    if (this.paymentForm.invalid || this.isProcessing) {
      return;
    }

    this.isProcessing = true;
    this.paymentResult = null;

    const formValue = this.paymentForm.value;
    const request: PaymentProcessRequest = {
      pan: formValue.pan.replace(/\s/g, ''),
      securityCode: formValue.securityCode,
      cardHolderName: formValue.cardHolderName,
      expirationDate: formValue.expirationDate
    };

    this.paymentService.processPayment(this.paymentId, request).subscribe({
      next: (response) => {
        this.isProcessing = false;
        this.paymentResult = {
          success: response.success,
          message: response.message
        };
        
        if (response.success) {
          this.paymentForm.disable();
        }
      },
      error: (err) => {
        this.isProcessing = false;
        this.paymentResult = {
          success: false,
          message: err.error?.message || 'Payment processing failed'
        };
      }
    });
  }

  getCardLogoPath(cardType: CardType | string): string {
    if (!cardType) return '';
    const type = typeof cardType === 'string' ? cardType : cardType;
    return `/assets/card-logos/${type.toLowerCase()}.svg`;
  }
}

