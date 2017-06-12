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

var BeakerXPageObject = require('./beakerx.po.js');
var beakerxPO;

describe('plotDemo page', function() {

    beforeEach(function(done) {
        beakerxPO = new BeakerXPageObject();
        browser
            .url(beakerxPO.baseURL)
            .call(done);
        beakerxPO.loginJupyter();
        browser.click('=plotDemo.ipynb');
        browser.window(browser.windowHandles().value[1]);
    });

    it('can run Groovy cell', function(done) {
        browser.waitForEnabled('i.kernel_idle_icon', 30000);
        browser.click('div.code_cell');
        beakerxPO.clickRunCell();
        browser.call(done);
    });

});