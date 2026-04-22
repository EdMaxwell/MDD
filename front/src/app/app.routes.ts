import { Routes } from '@angular/router';
import { AuthPageComponent } from './features/auth/auth-page.component';
import { ArticleCreatePageComponent } from './features/articles/article-create-page.component';
import { ArticleDetailPageComponent } from './features/articles/article-detail-page.component';
import { HomePageComponent } from './features/home/home-page.component';
import { ProfilePageComponent } from './features/profile/profile-page.component';
import { TopicsPageComponent } from './features/topics/topics-page.component';

/**
 * Client routes for the MDD single-page application.
 */
export const routes: Routes = [
  { path: '', component: AuthPageComponent, data: { screen: 'landing' } },
  { path: 'login', component: AuthPageComponent, data: { screen: 'login' } },
  { path: 'register', component: AuthPageComponent, data: { screen: 'register' } },
  { path: 'home', component: HomePageComponent },
  { path: 'articles/new', component: ArticleCreatePageComponent },
  { path: 'articles/:id', component: ArticleDetailPageComponent },
  { path: 'topics', component: TopicsPageComponent },
  { path: 'profile', component: ProfilePageComponent },
];
