import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { VehicleService, Vehicle } from '../../services/vehicle.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  vehicles = signal<Vehicle[]>([]);
  isLoading = signal(false);
  showModal = signal(false);
  isEditMode = signal(false);
  editingVehicleId = signal<number | null>(null);
  vehicleForm: FormGroup;
  errorMessage = signal('');
  successMessage = signal('');

  constructor(
    private vehicleService: VehicleService,
    private fb: FormBuilder,
  ) {
    this.vehicleForm = this.fb.group({
      pricePerDay: ['', [Validators.required, Validators.min(0.01)]],
      registration: ['', [Validators.required, Validators.minLength(2)]],
      chassisNumber: ['', [Validators.required, Validators.minLength(2)]],
      type: ['', [Validators.required, Validators.minLength(2)]],
      pictureUrl: ['']
    });
  }

  ngOnInit(): void {
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.vehicleService.getAllVehicles().subscribe({
      next: (vehicles) => {
        this.vehicles.set(vehicles || []);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading vehicles:', error);
        this.errorMessage.set(error.message || 'Greška pri učitavanju vozila.');
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.isEditMode.set(false);
    this.editingVehicleId.set(null);
    this.vehicleForm.reset();
    this.showModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  openEditModal(vehicle: Vehicle): void {
    this.isEditMode.set(true);
    this.editingVehicleId.set(vehicle.id || null);
    this.vehicleForm.patchValue({
      pricePerDay: vehicle.pricePerDay,
      registration: vehicle.registration,
      chassisNumber: vehicle.chassisNumber,
      type: vehicle.type,
      pictureUrl: vehicle.pictureUrl || ''
    });
    this.showModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeModal(): void {
    this.showModal.set(false);
    this.vehicleForm.reset();
    this.isEditMode.set(false);
    this.editingVehicleId.set(null);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  onSubmit(): void {
    if (this.vehicleForm.valid) {
      const vehicleData = this.vehicleForm.value;
      this.isLoading.set(true);
      this.errorMessage.set('');
      this.successMessage.set('');

      if (this.isEditMode() && this.editingVehicleId()) {
        this.vehicleService.updateVehicle((this.editingVehicleId() || 0), vehicleData).subscribe({
          next: () => {
            this.successMessage.set('Vozilo je uspješno ažurirano!');
            this.loadVehicles();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading.set(false);
            this.errorMessage.set(error.error?.message || 'Greška pri ažuriranju vozila.');
          }
        });
      } else {
        this.vehicleService.createVehicle(vehicleData).subscribe({
          next: () => {
            this.successMessage.set('Vozilo je uspješno kreirano!');
            this.loadVehicles();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading.set(false);
            this.errorMessage.set(error.error?.message || 'Greška pri kreiranju vozila.');
          }
        });
      }
    } else {
      this.markFormGroupTouched(this.vehicleForm);
    }
  }

  deleteVehicle(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovo vozilo?')) {
      this.isLoading.set(true);
      this.vehicleService.deleteVehicle(id).subscribe({
        next: () => {
          this.loadVehicles();
          this.successMessage.set('Vozilo je uspješno obrisano!');
          setTimeout(() => {
            this.successMessage.set('');
          }, 3000);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.error?.message || 'Greška pri brisanju vozila.');
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

  get pricePerDay() {
    return this.vehicleForm.get('pricePerDay');
  }

  get registration() {
    return this.vehicleForm.get('registration');
  }

  get chassisNumber() {
    return this.vehicleForm.get('chassisNumber');
  }

  get type() {
    return this.vehicleForm.get('type');
  }

  get pictureUrl() {
    return this.vehicleForm.get('pictureUrl');
  }

  trackByVehicleId(index: number, vehicle: Vehicle): number | undefined {
    return vehicle.id;
  }

  isDirectImageUrl(url: string | undefined): boolean {
    if (!url) return false;
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp'];
    const lowerUrl = url.toLowerCase();
    // Proveri da li URL završava sa ekstenzijom slike ili sadrži poznate image hosting servise
    return imageExtensions.some(ext => lowerUrl.endsWith(ext) || lowerUrl.includes(ext + '?')) ||
           lowerUrl.includes('i.imgur.com') ||
           lowerUrl.includes('images.unsplash.com') ||
           lowerUrl.includes('cdn.pexels.com') ||
           lowerUrl.includes('images.pexels.com');
  }

  onImageError(event: any): void {
    event.target.src = '/assets/icons/rent.png';
    event.target.onerror = null; // Spreči beskonačnu petlju
  }
}

