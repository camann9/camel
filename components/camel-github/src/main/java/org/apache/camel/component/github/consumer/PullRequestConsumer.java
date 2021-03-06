/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.github.consumer;

import java.util.List;
import java.util.Stack;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.github.GitHubEndpoint;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullRequestConsumer extends AbstractGitHubConsumer {
    private static final transient Logger LOG = LoggerFactory.getLogger(PullRequestConsumer.class);

    private PullRequestService pullRequestService;
    
    private int lastOpenPullRequest;

    public PullRequestConsumer(GitHubEndpoint endpoint, Processor processor) throws Exception {
        super(endpoint, processor);
        
        pullRequestService = new PullRequestService();
        initService(pullRequestService);

        LOG.info("GitHub PullRequestConsumer: Indexing current pull requests...");
        List<PullRequest> pullRequests = pullRequestService.getPullRequests(getRepository(), "open");
        if (pullRequests.size() > 0) {
            lastOpenPullRequest = pullRequests.get(0).getNumber();
        }
    }

    @Override
    protected int poll() throws Exception {
        List<PullRequest> openPullRequests = pullRequestService.getPullRequests(getRepository(), "open");
        // In the end, we want PRs oldest to newest.
        Stack<PullRequest> newPullRequests = new Stack<PullRequest>();
        for (PullRequest pullRequest : openPullRequests) {
            if (pullRequest.getNumber() > lastOpenPullRequest) {
                newPullRequests.push(pullRequest);
            } else {
                break;
            }
        }
        
        if (newPullRequests.size() > 0) {
            lastOpenPullRequest = openPullRequests.get(0).getNumber();
        }

        while (!newPullRequests.empty()) {
            PullRequest newPullRequest = newPullRequests.pop();
            Exchange e = getEndpoint().createExchange();
            e.getIn().setBody(newPullRequest);
            getProcessor().process(e);
        }
        return newPullRequests.size();
    }
}
