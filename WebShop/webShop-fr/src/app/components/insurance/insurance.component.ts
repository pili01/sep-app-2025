import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
  insuranceList: Insurance[] = [];
  isLoading = false;
  showModal = false;
  isEditMode = false;
  editingInsuranceId: number | null = null;
  insuranceForm: FormGroup;
  errorMessage = '';
  successMessage = '';

  constructor(
    private insuranceService: InsuranceService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
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
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();
    
    this.insuranceService.getAllInsurances()
      .then((insurances) => {
        this.insuranceList = insurances || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      })
      .catch((error) => {
        console.error('Error loading insurances:', error);
        this.errorMessage = 'Greška pri učitavanju osiguranja.';
        this.isLoading = false;
        this.cdr.detectChanges();
      });
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.editingInsuranceId = null;
    this.insuranceForm.reset();
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditModal(insurance: Insurance): void {
    this.isEditMode = true;
    this.editingInsuranceId = insurance.id || null;
    this.insuranceForm.patchValue({
      company: insurance.company,
      name: insurance.name || '',
      pricePerDay: insurance.pricePerDay
    });
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.insuranceForm.reset();
    this.isEditMode = false;
    this.editingInsuranceId = null;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.insuranceForm.valid) {
      const insuranceData = this.insuranceForm.value;
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      if (this.isEditMode && this.editingInsuranceId) {
        this.insuranceService.updateInsurance(this.editingInsuranceId, insuranceData).subscribe({
          next: () => {
            this.successMessage = 'Osiguranje je uspešno ažurirano!';
            this.loadInsurances();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading = false;
            this.errorMessage = error.error?.message || 'Greška pri ažuriranju osiguranja.';
          }
        });
      } else {
        this.insuranceService.createInsurance(insuranceData).subscribe({
          next: () => {
            this.successMessage = 'Osiguranje je uspešno kreirano!';
            this.loadInsurances();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading = false;
            this.errorMessage = error.error?.message || 'Greška pri kreiranju osiguranja.';
          }
        });
      }
    } else {
      this.markFormGroupTouched(this.insuranceForm);
    }
  }

  deleteInsurance(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovo osiguranje?')) {
      this.isLoading = true;
      this.insuranceService.deleteInsurance(id).subscribe({
        next: () => {
          this.loadInsurances();
          this.successMessage = 'Osiguranje je uspešno obrisano!';
          setTimeout(() => {
            this.successMessage = '';
          }, 3000);
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Greška pri brisanju osiguranja.';
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

