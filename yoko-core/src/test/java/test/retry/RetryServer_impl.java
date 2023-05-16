/*==============================================================================
 * Copyright 2010 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *=============================================================================*/
package test.retry;

public class RetryServer_impl extends RetryServerPOA {
    private org.omg.PortableServer.POA poa_;

    private Test test_;

    private Retry retry_;

    public RetryServer_impl(org.omg.PortableServer.POA poa, Test test,
            Retry retry) {
        poa_ = poa;
        test_ = test;
        retry_ = retry;
    }

    public org.omg.PortableServer.POA _default_POA() {
        if (poa_ != null)
            return poa_;
        else
            return super._default_POA();
    }

    public Test get_location_forward_object() {
        return test_;
    }

    public Retry get_retry_object() {
        return retry_;
    }

    public void deactivate() {
        _orb().shutdown(false);
    }
}
