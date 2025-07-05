package com.example.ootd.domain.message.entity;

import static lombok.AccessLevel.PROTECTED;

import com.example.ootd.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "messages")
public class Message {

  @Id
  @Column(columnDefinition = "uuid", nullable = false, updatable = false)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender", nullable = false)
  private User sender;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "receiver", nullable = false)
  private User receiver;
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "contents", length = 255)
  private String content;
  @Column(name = "dm_key", length = 255)
  private String dmKey;


  @Builder
  private Message(User sender, User receiver, String content) {
    this.id = UUID.randomUUID();
    this.sender = sender;
    this.receiver = receiver;
    this.content = content;
    this.dmKey = makeDmKey(sender.getId(), receiver.getId());
  }

  public static Message createMessage(User sender, User receiver, String content) {
    return Message.builder()
        .sender(sender)
        .receiver(receiver)
        .content(content)
        .build();
  }

  public static String makeDmKey(UUID first, UUID second) {
    String a = first.toString();
    String b = second.toString();

    if (a.compareTo(b) < 0) {
      return a + '_' + b;
    } else {
      return b + '_' + a;
    }
  }

}
