import ContactCard from "./ContactCard";

function ContactCardList(props) {

    const { contacts } = props; // destructuring : props = { contacts : [ {}, {}, {}] }

    return (
        <>
            {/* 
            <ContactCard contact={contacts[0]} />
            <ContactCard contact={contacts[1]} />
            <ContactCard contact={contacts[2]} /> 
            */}

            {
                contacts.map((c) => {
                    <ContactCard key={c.name} contact={c} />
                })
            }

        </>
    )

}

export default ContactCardList;