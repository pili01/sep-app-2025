import { Component, OnInit, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../service/payment.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-payment-methods',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment-methods.html',
  styleUrl: './payment-methods.scss',
})
export class PaymentMethods implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef;

  paymentMethods = signal<any[]>([]);
  selectedPaymentMethod = signal<any | null>(null);

  isModalOpen = signal(false);
  modalMode = signal<'ADD' | 'DETAILS' | 'EDIT'>('ADD');
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  selectedFile = signal<File | null>(null);
  iconPreview = signal<string | null>(null);
  uploadingFile = signal(false);

  modalTitle = computed(() => {
    const mode = this.modalMode();
    const methodName = this.selectedPaymentMethod()?.name;

    switch (mode) {
      case 'ADD':
        return 'Add New Payment Method';
      case 'EDIT':
        return `Edit: ${methodName}`;
      case 'DETAILS':
        return 'Payment Method Details';
      default:
        return 'Payment Methods';
    }
  });

  newPaymentMethod = {
    name: '',
    hostname: '',
    healthEndpoint: '',
    paymentEndpoint: '',
    paymentMethodCode: 'CUSTOM',
    active: true,
    enabled: true,
    iconPath: '',
  };

  paymentMethodCodes = [
    { value: 'CUSTOM', label: 'Custom' },
    { value: 'BANK_QR', label: 'Qr code' },
    { value: 'BANK_CARD', label: 'Bank Card' },
  ];

  constructor(private paymentService: PaymentService) {}

  ngOnInit() {
    this.loadPaymentMethods();
  }

  loadPaymentMethods() {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.paymentService.getAllPaymentMethods().subscribe({
      next: (data) => {
        this.paymentMethods.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load payment methods');
        this.loading.set(false);
      },
    });
  }

  openAddModal() {
    this.newPaymentMethod = {
      name: '',
      hostname: '',
      healthEndpoint: '',
      paymentEndpoint: '',
      paymentMethodCode: 'CUSTOM',
      active: true,
      enabled: true,
      iconPath: '',
    };
    this.selectedFile.set(null);
    this.iconPreview.set(null);
    this.modalMode.set('ADD');
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(method: any) {
    this.selectedPaymentMethod.set(method);
    this.newPaymentMethod = { ...method };
    this.selectedFile.set(null);
    this.iconPreview.set(method.iconPath ? this.getIconUrl(method.iconPath) : null);
    this.modalMode.set('EDIT');
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  openDetails(method: any) {
    this.selectedPaymentMethod.set(method);
    this.modalMode.set('DETAILS');
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  closeModal() {
    this.isModalOpen.set(false);
    this.selectedPaymentMethod.set(null);
    this.errorMessage.set(null);
    this.selectedFile.set(null);
    this.iconPreview.set(null);
  }

  onFileSelected(event: any) {
    const file = event.target.files?.[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.errorMessage.set('Please select a valid image file');
        return;
      }

      // Validate file size (max 2MB)
      if (file.size > 2 * 1024 * 1024) {
        this.errorMessage.set('File size must be less than 2MB');
        return;
      }

      this.selectedFile.set(file);
      this.errorMessage.set(null);

      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.iconPreview.set(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  }

  removeIcon() {
    this.selectedFile.set(null);
    this.iconPreview.set(null);
    this.newPaymentMethod.iconPath = '';
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  triggerFileInput() {
    this.fileInput.nativeElement.click();
  }

  onSave() {
    this.errorMessage.set(null);

    // Validate required fields
    if (
      !this.newPaymentMethod.name ||
      !this.newPaymentMethod.hostname ||
      !this.newPaymentMethod.healthEndpoint ||
      !this.newPaymentMethod.paymentEndpoint
    ) {
      this.errorMessage.set('All fields are required');
      return;
    }

    // Validate URLs
    if (!this.isValidUrl(this.newPaymentMethod.hostname)) {
      this.errorMessage.set('Invalid hostname URL');
      return;
    }
    if (!this.isValidUrl(this.newPaymentMethod.healthEndpoint)) {
      this.errorMessage.set('Invalid health endpoint URL');
      return;
    }
    if (!this.isValidUrl(this.newPaymentMethod.paymentEndpoint)) {
      this.errorMessage.set('Invalid payment endpoint URL');
      return;
    }

    this.loading.set(true);
    this.uploadingFile.set(false);

    // If file is selected, upload it first
    if (this.selectedFile()) {
      this.uploadingFile.set(true);
      this.paymentService.uploadPaymentMethodIcon(this.selectedFile()!).subscribe({
        next: (response: any) => {
          this.newPaymentMethod.iconPath = response.filename;
          this.proceedWithSave();
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message || 'Failed to upload icon');
          this.loading.set(false);
          this.uploadingFile.set(false);
        },
      });
    } else {
      this.proceedWithSave();
    }
  }

  private proceedWithSave() {
    const request = {
      name: this.newPaymentMethod.name,
      hostname: this.newPaymentMethod.hostname,
      statusUrl: this.newPaymentMethod.healthEndpoint,
      paymentUrl: this.newPaymentMethod.paymentEndpoint,
      paymentMethodCode: this.newPaymentMethod.paymentMethodCode,
      iconPath: this.newPaymentMethod.iconPath || null,
      active: this.newPaymentMethod.active,
      enabled: this.newPaymentMethod.enabled,
    };

    if (this.modalMode() === 'ADD') {
      this.paymentService.addPaymentMethod(request).subscribe({
        next: () => {
          this.loadPaymentMethods();
          this.closeModal();
          this.loading.set(false);
          this.uploadingFile.set(false);
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message || 'Failed to add payment method');
          this.loading.set(false);
          this.uploadingFile.set(false);
        },
      });
    } else if (this.modalMode() === 'EDIT') {
      this.paymentService.updatePaymentMethod(this.selectedPaymentMethod().id, request).subscribe({
        next: () => {
          this.loadPaymentMethods();
          this.closeModal();
          this.loading.set(false);
          this.uploadingFile.set(false);
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message || 'Failed to update payment method');
          this.loading.set(false);
          this.uploadingFile.set(false);
        },
      });
    }
  }

  onDelete(method: any) {
    if (confirm(`Are you sure you want to delete "${method.name}"?`)) {
      this.loading.set(true);
      this.paymentService.deletePaymentMethod(method.id).subscribe({
        next: () => {
          this.loadPaymentMethods();
          this.closeModal();
          this.loading.set(false);
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message || 'Failed to delete payment method');
          this.loading.set(false);
        },
      });
    }
  }

  toggleActive(method: any) {
    this.paymentService.togglePaymentMethodActive(method.id).subscribe({
      next: () => {
        this.loadPaymentMethods();
      },
      error: (err: any) => {
        this.errorMessage.set('Failed to update payment method status');
      },
    });
  }

  private isValidUrl(url: string): boolean {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  }

  getMethodCode(codeValue: string): string {
    const code = this.paymentMethodCodes.find((c) => c.value === codeValue);
    return code?.label || codeValue;
  }

  getIconUrl(iconPath: string | null): string {
    if (!iconPath) return '';
    return `${environment.iconBaseUrl}/payment-methods/icon/${iconPath}`;
  }
}
