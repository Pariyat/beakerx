/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

declare function require(moduleName: string): any;
const widgets = require('jupyter-js-widgets');

import { TEXT_INPUT_WIDTH_UNIT } from './textWidget';

const TEXT_INPUT_HEIGHT_UNIT = 'em';
const LINE_HEIGHT_FACTOR = 1.3;

class TextareaModel extends widgets.TextareaModel {
  defaults() {
    return {
      ...super.defaults(),
      _view_name: "TextareaView",
      _model_name: "TextareaModel",
      _model_module: 'beakerx',
      _view_module: 'beakerx'
    };
  }
}

class TextareaView extends widgets.TextareaView {
  render() {
    super.render.call(this);

    const width = this.model.get('width');
    const height = this.model.get('height');

    if (width >= 0) {
      this.textbox.style.maxWidth = width + TEXT_INPUT_WIDTH_UNIT;
    }

    if (height >= 0) {
      this.textbox.style.lineHeight = LINE_HEIGHT_FACTOR + TEXT_INPUT_HEIGHT_UNIT;
      this.textbox.style.height = (LINE_HEIGHT_FACTOR * height) + TEXT_INPUT_HEIGHT_UNIT;
    }
  }
}

export default {
  TextareaModel,
  TextareaView
};
