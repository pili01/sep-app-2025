import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { InsuranceService, Insurance } from '../../services/insurance.service';

@Component({
  selector: 'app-insurance',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './insurance.component.html',
  styleUrl: './insurance.component.scss'
})
export class InsuranceComponent implements OnInit {
  insuranceList= signal<Insurance[]>([]);
  isLoading = signal(false);
  showModal = signal(false);
  isEditMode = signal(false);
  editingInsuranceId = signal<number | null>(null);
  insuranceForm: FormGroup;
  errorMessage = signal('');
  successMessage = signal('');

  constructor(
    private insuranceService: InsuranceService,
    private fb: FormBuilder,
  ) {
    this.insuranceForm = this.fb.group({
      company: ['', [Validators.required, Validators.minLength(2)]],
      name: [''],
      pricePerDay: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.loadInsurances();
  }

  loadInsurances(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.insuranceService.getAllInsurances().subscribe({
      next: (insurances) => {
        this.insuranceList.set(insurances || []);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading insurances:', error);
        this.errorMessage.set(error.message || 'Greška pri učitavanju osiguranja.');
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.isEditMode.set(false);
    this.editingInsuranceId.set(null);
    this.insuranceForm.reset();
    this.showModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  openEditModal(insurance: Insurance): void {
    this.isEditMode.set(true);
    this.editingInsuranceId.set(insurance.id || null);
    this.insuranceForm.patchValue({
      company: insurance.company,
      name: insurance.name || '',
      pricePerDay: insurance.pricePerDay
    });
    this.showModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeModal(): void {
    this.showModal.set(false);
    this.insuranceForm.reset();
    this.isEditMode.set(false);
    this.editingInsuranceId.set(null);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  onSubmit(): void {
    if (this.insuranceForm.valid) {
      const insuranceData = this.insuranceForm.value;
      this.isLoading.set(true);
      this.errorMessage.set('');
      this.successMessage.set('');

      if (this.isEditMode() && this.editingInsuranceId()) {
        this.insuranceService.updateInsurance(this.editingInsuranceId()!, insuranceData).subscribe({
          next: () => {
            this.successMessage.set('Osiguranje je uspješno ažurirano!');
            this.loadInsurances();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading.set(false);
            this.errorMessage.set(error.error?.message || 'Greška pri ažuriranju osiguranja.');
          }
        });
      } else {
        this.insuranceService.createInsurance(insuranceData).subscribe({
          next: () => {
            this.successMessage.set('Osiguranje je uspješno kreirano!');
            this.loadInsurances();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading.set(false);
            this.errorMessage.set(error.error?.message || 'Greška pri kreiranju osiguranja.');
          }
        });
      }
    } else {
      this.markFormGroupTouched(this.insuranceForm);
    }
  }

  deleteInsurance(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovo osiguranje?')) {
      this.isLoading.set(true);
      this.insuranceService.deleteInsurance(id).subscribe({
        next: () => {
          this.loadInsurances();
          this.successMessage.set('Osiguranje je uspješno obrisano!');
          setTimeout(() => {
            this.successMessage.set('');
          }, 3000);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.error?.message || 'Greška pri brisanju osiguranja.');
        }
      });
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  get company() {
    return this.insuranceForm.get('company');
  }

  get name() {
    return this.insuranceForm.get('name');
  }

  get pricePerDay() {
    return this.insuranceForm.get('pricePerDay');
  }
}

