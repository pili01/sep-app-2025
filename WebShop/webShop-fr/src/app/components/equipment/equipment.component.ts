import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { EquipmentService, Equipment } from '../../services/equipment.service';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './equipment.component.html',
  styleUrl: './equipment.component.scss'
})
export class EquipmentComponent implements OnInit {
  equipmentList: Equipment[] = [];
  isLoading = false;
  showModal = false;
  isEditMode = false;
  editingEquipmentId: number | null = null;
  equipmentForm: FormGroup;
  errorMessage = '';
  successMessage = '';

  constructor(
    private equipmentService: EquipmentService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.equipmentForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      pricePerDay: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.loadEquipment();
  }

  loadEquipment(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();
    
    this.equipmentService.getAllEquipment()
      .then((equipment) => {
        this.equipmentList = equipment || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      })
      .catch((error) => {
        console.error('Error loading equipment:', error);
        this.errorMessage = 'Greška pri učitavanju opreme.';
        this.isLoading = false;
        this.cdr.detectChanges();
      });
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.editingEquipmentId = null;
    this.equipmentForm.reset();
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditModal(equipment: Equipment): void {
    this.isEditMode = true;
    this.editingEquipmentId = equipment.id || null;
    this.equipmentForm.patchValue({
      name: equipment.name,
      description: equipment.description || '',
      pricePerDay: equipment.pricePerDay
    });
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.equipmentForm.reset();
    this.isEditMode = false;
    this.editingEquipmentId = null;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.equipmentForm.valid) {
      const equipmentData = this.equipmentForm.value;
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      if (this.isEditMode && this.editingEquipmentId) {
        this.equipmentService.updateEquipment(this.editingEquipmentId, equipmentData).subscribe({
          next: () => {
            this.successMessage = 'Oprema je uspešno ažurirana!';
            this.loadEquipment();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading = false;
            this.errorMessage = error.error?.message || 'Greška pri ažuriranju opreme.';
          }
        });
      } else {
        this.equipmentService.createEquipment(equipmentData).subscribe({
          next: () => {
            this.successMessage = 'Oprema je uspešno kreirana!';
            this.loadEquipment();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading = false;
            this.errorMessage = error.error?.message || 'Greška pri kreiranju opreme.';
          }
        });
      }
    } else {
      this.markFormGroupTouched(this.equipmentForm);
    }
  }

  deleteEquipment(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovu opremu?')) {
      this.isLoading = true;
      this.equipmentService.deleteEquipment(id).subscribe({
        next: () => {
          this.loadEquipment();
          this.successMessage = 'Oprema je uspešno obrisana!';
          setTimeout(() => {
            this.successMessage = '';
          }, 3000);
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Greška pri brisanju opreme.';
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

  get name() {
    return this.equipmentForm.get('name');
  }

  get description() {
    return this.equipmentForm.get('description');
  }

  get pricePerDay() {
    return this.equipmentForm.get('pricePerDay');
  }
}

