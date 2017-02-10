import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule } from '@angular/material';

import 'hammerjs';
import { AppComponent, DialogContent } from './app.component';

@NgModule({
  declarations: [
    AppComponent, DialogContent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    MaterialModule.forRoot()
  ],
  providers: [],
  entryComponents: [DialogContent],
  bootstrap: [AppComponent]
})
export class AppModule { }
