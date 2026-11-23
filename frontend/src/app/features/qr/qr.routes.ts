import { Routes } from '@angular/router';
import { CreateQrComponent } from './pages/create-qr/create-qr.component';
import { QrCreatedComponent } from './pages/qr-created/qr-created.component';
import { QrDetailComponent } from './pages/qr-detail/qr-detail.component';
import { QrListComponent } from './pages/qr-list/qr-list.component';

export const QR_ROUTES: Routes = [
  { path: '', component: QrListComponent },
  { path: 'new', component: CreateQrComponent },
  { path: 'created', component: QrCreatedComponent },
  { path: ':id/edit', component: CreateQrComponent },
  { path: ':id', component: QrDetailComponent },
];
