import {Component} from '@angular/core';
import {AppService, Recipe, RecipeStep} from "./app.service";
import {MdSnackBar, MdSnackBarRef} from '@angular/material';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [AppService]
})
export class AppComponent {
  source: string = null;
  selected: number = -1;
  recipes: Recipe[] = [];

  private snackBar: MdSnackBarRef<any> = null;

  constructor(private service: AppService, private snackBarService: MdSnackBar) {
    this.service.recipes.subscribe(recipes => {
      this.updateRecipes(recipes ? recipes : []);
    });
    this.service.events.subscribe(event => {
      if (event.type == 'busy') {
        this.alert('Already busy with previous action.');
      } else if (event.type == 'connection-error') {
        this.alert('Cannot connect to Vamp.');
      }
    });
  }

  wide() {
    return window.screen.width >= 768;
  }

  updateRecipes(recipes: Recipe[]) {
    for (let i = this.recipes.length - 1; i >= 0; i--) {
      let found = null;
      for (let j = 0; j < recipes.length; j++) {
        if (this.recipes[i].id == recipes[j].id) {
          found = recipes[j];
          break;
        }
      }
      this.recipes.splice(i, 1, found);
    }

    for (let i = 0; i < recipes.length; i++) {
      let found = null;
      for (let j = 0; j < this.recipes.length; j++) {
        if (this.recipes[j].id == recipes[i].id) {
          found = this.recipes[j];
          break;
        }
      }
      if (!found) {
        this.recipes.push(recipes[i]);
      }
    }

    if (this.recipes.length > 0) {
      if (this.selected < 0) {
        this.selected = 0;
      }
    } else {
      this.selected = -1;
    }
  }

  showRecipe(index: number) {
    this.source = null;
    this.selected = index;
  }

  showSource(step?: RecipeStep) {
    this.source = step ? step.resource : null;
  }

  run(step: RecipeStep) {
    if (this.selected >= 0) {
      this.service.run(this.recipes[this.selected], step);
      this.alert('Running: "' + step.description.toLocaleLowerCase() + '".');
    }
  }

  reset() {
    if (this.selected >= 0) {
      this.service.cleanup(this.recipes[this.selected]);
      this.alert('Resetting: "' + this.recipes[this.selected].name.toLocaleLowerCase() + '".');
    }
  }

  isRunnable(step): boolean {
    if (this.selected < 0) return false;
    if (step.dirty) return true;

    let runnable = true;
    for (let i = 0; i < this.recipes[this.selected].run.length; i++) {
      let s = this.recipes[this.selected].run[i];
      if (s.id == step.id)
        return runnable;
      else {
        runnable = runnable && s.dirty;
        if (!runnable) return false;
      }
    }
    return false;
  }

  progress(): number {
    if (this.selected < 0) return 0;

    let count = 0;
    let total = this.recipes[this.selected].cleanup.length;

    for (let i = 0; i < total; i++) {
      if (this.recipes[this.selected].cleanup[i].state == 'succeeded') count++;
    }

    return total == 0 ? 0 : Math.ceil(count / total * 100);
  }

  private alert(message: string) {
    let $this = this;
    if (this.snackBar) {
      this.snackBar.dismiss();
      this.snackBar.afterDismissed().subscribe(() => {
          $this.snackBar = this.snackBarService.open(message, null, {
            duration: 3000,
          });
        }
      );
    } else {
      this.snackBar = this.snackBarService.open(message, null, {
        duration: 3000,
      });
    }
  }
}
