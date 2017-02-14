import {Injectable, EventEmitter} from '@angular/core';
import {environment} from '../environments/environment';
import {$WebSocket, WebSocketSendMode} from './websocket.service';

@Injectable()
export class RunnerService {

  private websocket;
  public recipes$: EventEmitter<Recipe[]>;

  constructor() {
    this.recipes$ = new EventEmitter();
    this.websocket = new $WebSocket(RunnerService.url(), null, {
      initialTimeout: 1000,
      maxTimeout: 3600000,
      reconnectIfNotNormalClose: true
    });
    this.websocket.getDataStream().subscribe((message: MessageEvent) => {
      this.process(JSON.parse(message.data));
    });
    this.websocket.onOpen(() => {
      this.command('recipes');
    });
  }

  run(recipe, step) {
    if (recipe) {
      this.command('run', {recipe: recipe.id, step: step.id});
    }
  };

  cleanup(recipe) {
    this.command('cleanup', [recipe.id]);
  };

  private command(cmd: string, args?: any) {
    let message = {
      command: cmd,
      arguments: args
    };
    this.websocket.send(message, WebSocketSendMode.Direct);
  }

  private process(payload: any) {
    if (payload.type == 'recipes') {
      let recipes: Recipe[] = [];
      for (let recipe of payload.recipes) {
        recipes.push(recipe);
      }
      this.recipes$.emit(recipes);
    }
  }

  private static url() {
    if (environment.wsUrl != null) {
      return 'ws://' + environment.wsUrl + '/channel';
    } else {
      let url = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
      url += window.location.host;
      url += window.location.pathname.charAt(window.location.pathname.length - 1) == '/' ?
        window.location.pathname + 'channel' :
        window.location.pathname + '/channel';
      return url;
    }
  }
}

export class Recipe {
  public id: string;
  public name: string;
  public description: string;
  public run: RecipeStep[];
  public cleanup: RecipeStep[];
}

export class RecipeStep {
  public id: string;
  public description: string;
  public resource: string;
  public dirty: boolean;
  public state: string;
}
