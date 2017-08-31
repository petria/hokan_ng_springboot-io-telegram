package org.freakz.hokan_ng_springboot.bot.io.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by Petri Airio on 17.5.2017.
 */
@Configuration
@PropertySource("file:telegram.properties")
@Getter
@Setter
public class TelegramConfiguration {


}
