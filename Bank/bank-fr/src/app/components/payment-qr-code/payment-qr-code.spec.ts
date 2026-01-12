import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaymentQrCode } from './payment-qr-code';

describe('PaymentQrCode', () => {
  let component: PaymentQrCode;
  let fixture: ComponentFixture<PaymentQrCode>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentQrCode]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PaymentQrCode);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
