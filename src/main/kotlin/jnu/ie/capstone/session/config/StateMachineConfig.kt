package jnu.ie.capstone.session.config

import jnu.ie.capstone.session.enums.SessionEvent
import jnu.ie.capstone.session.enums.SessionState
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.config.EnableStateMachineFactory
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer

@Configuration
@EnableStateMachineFactory
class StateMachineConfig : EnumStateMachineConfigurerAdapter<SessionState, SessionEvent>() {

    override fun configure(states: StateMachineStateConfigurer<SessionState, SessionEvent>) {
        states.withStates()
            .initial(SessionState.MENU_SELECTION)
            .states(SessionState.entries.toSet())
    }

    override fun configure(transitions: StateMachineTransitionConfigurer<SessionState, SessionEvent>) {
        transitions
            .withExternal()
            .source(SessionState.MENU_SELECTION)
            .target(SessionState.CART_CONFIRMATION)
            .event(SessionEvent.CONFIRM_CART)
            .and()
            .withExternal()
            .source(SessionState.CART_CONFIRMATION)
            .target(SessionState.PAYMENT_CONFIRMATION)
            .event(SessionEvent.CONFIRM_PAYMENT)
            .and()
            .withExternal()
            .source(SessionState.PAYMENT_CONFIRMATION)
            .target(SessionState.COMPLETED)
            .event(SessionEvent.PROCESS_PAYMENT)
            .and()
            .withExternal()
            .source(SessionState.PAYMENT_CONFIRMATION)
            .target(SessionState.MENU_SELECTION)
            .event(SessionEvent.PREVIOUS)
            .and()
            .withExternal()
            .source(SessionState.CART_CONFIRMATION)
            .target(SessionState.MENU_SELECTION)
            .event(SessionEvent.PREVIOUS)
            .and()
            .withExternal()
            .source(SessionState.PAYMENT_CONFIRMATION)
            .target(SessionState.CANCELLED)
            .event(SessionEvent.CANCEL)
            .and()
            .withExternal()
            .source(SessionState.CART_CONFIRMATION)
            .target(SessionState.CANCELLED)
            .event(SessionEvent.CANCEL)
            .and()
            .withExternal()
            .source(SessionState.MENU_SELECTION)
            .target(SessionState.CANCELLED)
            .event(SessionEvent.CANCEL)
    }

}