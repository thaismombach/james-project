/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/


package org.apache.mailet.base.test;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class FakeMail implements Mail {

    public static FakeMail fromMime(String text, String javaEncodingCharset, String javamailDefaultEncodingCharset) throws MessagingException, UnsupportedEncodingException {
        Properties javamailProperties = new Properties();
        javamailProperties.setProperty("mail.mime.charset", javamailDefaultEncodingCharset);
        return FakeMail.builder()
                .mimeMessage(new MimeMessage(
                    Session.getInstance(javamailProperties),
                    new ByteArrayInputStream(text.getBytes(javaEncodingCharset))))
                .build();
    }

    public static FakeMail fromMail(Mail mail) throws MessagingException {
        return new FakeMail(mail.getMessage(),
            Lists.newArrayList(mail.getRecipients()),
            mail.getName(),
            mail.getSender(),
            mail.getState(),
            mail.getErrorMessage(),
            mail.getLastUpdated(),
            attributes(mail),
            mail.getMessageSize(),
            mail.getRemoteAddr());
    }

    public static FakeMail from(MimeMessage message) throws MessagingException {
        return builder()
                .mimeMessage(message)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Optional<String> fileName;
        private Optional<MimeMessage> mimeMessage;
        private List<MailAddress> recipients;
        private Optional<String> name;
        private Optional<MailAddress> sender;
        private Optional<String> state;
        private Optional<String> errorMessage;
        private Optional<Date> lastUpdated;
        private Map<String, Serializable> attributes;
        private Optional<Long> size;
        private Optional<String> remoteAddr;

        private Builder() {
            fileName = Optional.absent();
            mimeMessage = Optional.absent();
            recipients = Lists.newArrayList();
            name = Optional.absent();
            sender = Optional.absent();
            state = Optional.absent();
            errorMessage = Optional.absent();
            lastUpdated = Optional.absent();
            attributes = Maps.newHashMap();
            size = Optional.absent();
            remoteAddr = Optional.absent();
        }

        public Builder size(long size) {
            this.size = Optional.of(size);
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = Optional.of(fileName);
            return this;
        }

        public Builder mimeMessage(MimeMessage mimeMessage) {
            this.mimeMessage = Optional.of(mimeMessage);
            return this;
        }

        public Builder recipients(List<MailAddress> recipients) {
            this.recipients.addAll(recipients);
            return this;
        }

        public Builder recipients(MailAddress... recipients) {
            this.recipients.addAll(ImmutableList.copyOf(recipients));
            return this;
        }

        public Builder recipient(MailAddress recipient) {
            this.recipients.add(recipient);
            return this;
        }

        public Builder name(String name) {
            this.name = Optional.of(name);
            return this;
        }

        public Builder sender(MailAddress sender) {
            this.sender = Optional.of(sender);
            return this;
        }

        public Builder state(String state) {
            this.state = Optional.of(state);
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = Optional.of(errorMessage);
            return this;
        }

        public Builder lastUpdated(Date lastUpdated) {
            this.lastUpdated = Optional.of(lastUpdated);
            return this;
        }

        public Builder attribute(String name, Serializable object) {
            this.attributes.put(name, object);
            return this;
        }

        public Builder attributes(Map<String, Serializable> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        public Builder remoteAddr(String remoteAddr) {
            this.remoteAddr = Optional.of(remoteAddr);
            return this;
        }

        public FakeMail build() throws MessagingException {
            return new FakeMail(getMimeMessage(), recipients, name.orNull(), sender.orNull(), state.orNull(), errorMessage.orNull(), lastUpdated.orNull(),
                    attributes, size.or(0l), remoteAddr.or("127.0.0.1"));
        }

        private MimeMessage getMimeMessage() throws MessagingException {
            Preconditions.checkState(!(fileName.isPresent() && mimeMessage.isPresent()), "You can not specify a MimeMessage object when you alredy set Content from a file");
            if (fileName.isPresent()) {
                return new MimeMessage(Session.getInstance(new Properties()), ClassLoader.getSystemResourceAsStream(fileName.get()));
            }
            return mimeMessage.orNull();
        }
    }

    public static FakeMail defaultFakeMail() throws MessagingException {
        return FakeMail.builder().build();
    }

    private static Map<String, Serializable> attributes(Mail mail) {
        ImmutableMap.Builder<String, Serializable> builder = ImmutableMap.builder();
        for (String attributeName: ImmutableList.copyOf(mail.getAttributeNames())) {
            builder.put(attributeName, mail.getAttribute(attributeName));
        }
        return builder.build();
    }

    private MimeMessage msg;
    private Collection<MailAddress> recipients;
    private String name;
    private MailAddress sender;
    private String state;
    private String errorMessage;
    private Date lastUpdated;
    private Map<String, Serializable> attributes;
    private long size;
    private String remoteAddr;
    
    public FakeMail(MimeMessage msg, List<MailAddress> recipients, String name, MailAddress sender, String state, String errorMessage, Date lastUpdated,
            Map<String, Serializable> attributes, long size, String remoteAddr) {
        this.msg = msg;
        this.recipients = recipients;
        this.name = name;
        this.sender = sender;
        this.state = state;
        this.errorMessage = errorMessage;
        this.lastUpdated = lastUpdated;
        this.attributes = attributes;
        this.size = size;
        this.remoteAddr = remoteAddr;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public MimeMessage getMessage() throws MessagingException {
        return msg;
    }

    @Override
    public Collection<MailAddress> getRecipients() {
        return recipients;
    }

    @Override
    public void setRecipients(Collection<MailAddress> recipients) {
        this.recipients = recipients;
    }

    @Override
    public MailAddress getSender() {
        return sender;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getRemoteHost() {
        return "111.222.333.444";
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    @Override
    public void setMessage(MimeMessage message) {
        this.msg = message;
        try {
            if (message != null && message.getSender() != null) {
                this.sender = new MailAddress((InternetAddress) message.getSender());
            }
        } catch (MessagingException e) {
            throw new RuntimeException("Exception caught", e);
        }
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public Serializable getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Iterator<String> getAttributeNames() {
        return attributes.keySet().iterator();
    }

    @Override
    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    @Override
    public Serializable removeAttribute(String name) {
        return attributes.remove(name);

    }

    @Override
    public void removeAllAttributes() {
        attributes.clear();
    }

    @Override
    public Serializable setAttribute(String name, Serializable object) {
        return attributes.put(name, object);
    }

    @Override
    public long getMessageSize() throws MessagingException {
        return size;
    }

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setMessageSize(long size) {
        this.size = size;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof FakeMail) {
            FakeMail that = (FakeMail) o;

            return Objects.equal(this.size, that.size)
                && Objects.equal(this.msg, that.msg)
                && Objects.equal(this.recipients, that.recipients)
                && Objects.equal(this.name, that.name)
                && Objects.equal(this.sender, that.sender)
                && Objects.equal(this.state, that.state)
                && Objects.equal(this.errorMessage, that.errorMessage)
                && Objects.equal(this.lastUpdated, that.lastUpdated)
                && Objects.equal(this.attributes, that.attributes)
                && Objects.equal(this.remoteAddr, that.remoteAddr);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(msg, name, sender, recipients, state, errorMessage, lastUpdated, attributes, size, recipients, remoteAddr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("msg", msg)
            .add("recipients", recipients)
            .add("name", name)
            .add("sender", sender)
            .add("state", state)
            .add("errorMessage", errorMessage)
            .add("lastUpdated", lastUpdated)
            .add("attributes", attributes)
            .add("size", size)
            .add("remoteAddr", remoteAddr)
            .toString();
    }
}
