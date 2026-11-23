import { Routes } from '@angular/router';
import { CreateCampaignComponent } from './pages/create-campaign/create-campaign.component';
import { CampaignDetailComponent } from './pages/campaign-detail/campaign-detail.component';
import { CampaignListComponent } from './pages/campaign-list/campaign-list.component';

export const CAMPAIGN_ROUTES: Routes = [
  { path: '', component: CampaignListComponent },
  { path: 'new', component: CreateCampaignComponent },
  { path: ':id', component: CampaignDetailComponent },
];
