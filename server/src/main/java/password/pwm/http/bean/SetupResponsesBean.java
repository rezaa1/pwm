/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2021 The PWM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package password.pwm.http.bean;

import com.novell.ldapchai.cr.Challenge;
import lombok.Data;
import lombok.EqualsAndHashCode;
import password.pwm.config.option.SessionBeanMode;
import password.pwm.http.servlet.setupresponses.ResponseMode;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Data
@EqualsAndHashCode( callSuper = false )
public class SetupResponsesBean extends PwmSessionBean
{
    private boolean hasExistingResponses;
    private final Map<ResponseMode, SetupData> challengeData = new HashMap<>();
    private final Set<ResponseMode> responsesSatisfied = new HashSet<>();
    private boolean confirmed;
    private Locale userLocale;
    private boolean initialized;

    @Override
    public BeanType getBeanType( )
    {
        return BeanType.AUTHENTICATED;
    }

    @Data
    @EqualsAndHashCode( callSuper = false )
    public static class SetupData implements Serializable
    {
        private Map<String, Challenge> indexedChallenges = Collections.emptyMap();
        private boolean simpleMode;
        private int minRandomSetup;
        private Map<Challenge, String> responseMap = Collections.emptyMap();
    }

    @Override
    public Set<SessionBeanMode> supportedModes( )
    {
        return Set.of( SessionBeanMode.LOCAL );
    }
}
