import { Routes } from '@angular/router';
import { AuthPageComponent } from './features/auth/auth-page.component';
import { HomePageComponent } from './features/home/home-page.component';

export const routes: Routes = [
  { path: '', component: AuthPageComponent, data: { screen: 'landing' } },
  { path: 'login', component: AuthPageComponent, data: { screen: 'login' } },
  { path: 'register', component: AuthPageComponent, data: { screen: 'register' } },
  { path: 'home', component: HomePageComponent },
];
