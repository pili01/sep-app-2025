import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PayWithQr } from './pay-with-qr';

describe('PayWithQr', () => {
  let component: PayWithQr;
  let fixture: ComponentFixture<PayWithQr>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PayWithQr]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PayWithQr);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
