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
package org.omg.CSIIOP;


/**
* org/omg/CSIIOP/ServiceConfiguration.java .
* Error reading Messages File.
* Error reading Messages File.
* Thursday, January 14, 2010 1:08:59 AM PST
*/

public final class ServiceConfiguration implements org.omg.CORBA.portable.IDLEntity
{
  public int syntax = (int)0;
  public byte name[] = null;

  public ServiceConfiguration ()
  {
  } // ctor

  public ServiceConfiguration (int _syntax, byte[] _name)
  {
    syntax = _syntax;
    name = _name;
  } // ctor

} // class ServiceConfiguration
