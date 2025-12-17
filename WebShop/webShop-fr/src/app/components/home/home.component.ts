import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
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
  vehicles: Vehicle[] = [];
  isLoading = false;
  showModal = false;
  isEditMode = false;
  editingVehicleId: number | null = null;
  vehicleForm: FormGroup;
  errorMessage = '';
  successMessage = '';

  constructor(
    private vehicleService: VehicleService,
    private authService: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
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
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();
    
    this.vehicleService.getAllVehicles()
      .then((vehicles) => {
        this.vehicles = vehicles || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      })
      .catch((error) => {
        console.error('Error loading vehicles:', error);
        this.errorMessage = 'Greška pri učitavanju vozila.';
        this.isLoading = false;
        this.cdr.detectChanges();
      });
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.editingVehicleId = null;
    this.vehicleForm.reset();
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditModal(vehicle: Vehicle): void {
    this.isEditMode = true;
    this.editingVehicleId = vehicle.id || null;
    this.vehicleForm.patchValue({
      pricePerDay: vehicle.pricePerDay,
      registration: vehicle.registration,
      chassisNumber: vehicle.chassisNumber,
      type: vehicle.type,
      pictureUrl: vehicle.pictureUrl || ''
    });
    this.showModal = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeModal(): void {
    this.showModal = false;
    this.vehicleForm.reset();
    this.isEditMode = false;
    this.editingVehicleId = null;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.vehicleForm.valid) {
      const vehicleData = this.vehicleForm.value;
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      if (this.isEditMode && this.editingVehicleId) {
        this.vehicleService.updateVehicle(this.editingVehicleId, vehicleData).subscribe({
          next: () => {
            this.successMessage = 'Vozilo je uspešno ažurirano!';
            this.loadVehicles();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading = false;
            this.errorMessage = error.error?.message || 'Greška pri ažuriranju vozila.';
          }
        });
      } else {
        this.vehicleService.createVehicle(vehicleData).subscribe({
          next: () => {
            this.successMessage = 'Vozilo je uspešno kreirano!';
            this.loadVehicles();
            setTimeout(() => {
              this.closeModal();
            }, 1500);
          },
          error: (error) => {
            this.isLoading = false;
            this.errorMessage = error.error?.message || 'Greška pri kreiranju vozila.';
          }
        });
      }
    } else {
      this.markFormGroupTouched(this.vehicleForm);
    }
  }

  deleteVehicle(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovo vozilo?')) {
      this.isLoading = true;
      this.vehicleService.deleteVehicle(id).subscribe({
        next: () => {
          this.loadVehicles();
          this.successMessage = 'Vozilo je uspešno obrisano!';
          setTimeout(() => {
            this.successMessage = '';
          }, 3000);
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Greška pri brisanju vozila.';
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

