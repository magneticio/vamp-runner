import {Component} from '@angular/core';
import {RunnerService, Recipe, RecipeStep} from './runner.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [RunnerService]
})
export class AppComponent {
  progress: number = 50;
  source: string = null;
  recipe: Recipe = null;
  recipes: Recipe[] = [];

  constructor(private runner: RunnerService) {
    runner.recipes$.subscribe(recipes => {
      this.recipes = recipes ? recipes : [];
      if (this.recipe == null && this.recipes && this.recipes.length > 0) {
        this.recipe = recipes[0];
      }
    });
  }

  showRecipe(recipe: Recipe) {
    this.source = null;
    this.recipe = recipe;
  }

  showSource(step: RecipeStep) {
    this.source = step.resource;
  }
}
