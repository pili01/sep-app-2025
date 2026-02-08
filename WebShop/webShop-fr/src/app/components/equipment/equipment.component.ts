import { Component, OnInit, signal } from '@angular/core';
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
  equipmentList = signal<Equipment[]>([]);
  isLoading = signal(false);
  showModal = signal(false);
  isEditMode = signal(false);
  editingEquipmentId = signal<number | null>(null);
  equipmentForm: FormGroup;
  errorMessage = signal('');
  successMessage = signal('');

  constructor(
    private equipmentService: EquipmentService,
    private fb: FormBuilder,
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
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.equipmentService.getAllEquipment().subscribe({
      next: (equipment) => {
        this.equipmentList.set(equipment || []);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading equipment:', error);
        this.errorMessage.set(error.message || 'Greška pri učitavanju opreme.');
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.isEditMode.set(false);
    this.editingEquipmentId.set(null);
    this.equipmentForm.reset();
    this.showModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  openEditModal(equipment: Equipment): void {
    this.isEditMode.set(true);
    this.editingEquipmentId.set(equipment.id || null);
    this.equipmentForm.patchValue({
      name: equipment.name,
      description: equipment.description || '',
      pricePerDay: equipment.pricePerDay
    });
    this.showModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeModal(): void {
    this.showModal.set(false);
    this.equipmentForm.reset();
    this.isEditMode.set(false);
    this.editingEquipmentId.set(null);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  onSubmit(): void {
    if (this.equipmentForm.valid) {
      const equipmentData = this.equipmentForm.value;
      this.isLoading.set(true);
      this.errorMessage.set('');
      this.successMessage.set('');

      if (this.isEditMode() && this.editingEquipmentId()) {
        this.equipmentService.updateEquipment((this.editingEquipmentId() || 0), equipmentData).subscribe({
          next: () => {
            this.successMessage.set('Oprema je uspješno ažurirana!');
            this.loadEquipment();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading.set(false);
            this.errorMessage.set(error.error?.message || 'Greška pri ažuriranju opreme.');
          }
        });
      } else {
        this.equipmentService.createEquipment(equipmentData).subscribe({
          next: () => {
            this.successMessage.set('Oprema je uspješno kreirana!');
            this.loadEquipment();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading.set(false);
            this.errorMessage.set(error.error?.message || 'Greška pri kreiranju opreme.');
          }
        });
      }
    } else {
      this.markFormGroupTouched(this.equipmentForm);
    }
  }

  deleteEquipment(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovu opremu?')) {
      this.isLoading.set(true);
      this.equipmentService.deleteEquipment(id).subscribe({
        next: () => {
          this.loadEquipment();
          this.successMessage.set('Oprema je uspješno obrisana!');
          setTimeout(() => {
            this.successMessage.set('');
          }, 3000);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.error?.message || 'Greška pri brisanju opreme.');
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

